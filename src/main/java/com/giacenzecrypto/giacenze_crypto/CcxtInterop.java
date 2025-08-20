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
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.json.JSONObject;

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

        Path downloadPath = Paths.get(Statiche.getWorkingDirectory()+"tools", filename);
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
    
    

    public static void fetchMovimenti(String exchangeId, String apiKey, String secret, String startDate,String Tokens) {
    try {
        Path nodePath = getNodeExePath();
        Path scriptPath = Paths.get("src/main/resources/js/"
                + "Binance_Trades"
                + ".js");

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
                exchangeId, apiKey, secret, startDate,Tokens
        );
        builder.directory(scriptPath.getParent().toFile());
        // Non reindirizziamo stderr su stdout
        // builder.redirectErrorStream(true);
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

        // Thread per log (stderr)
        new Thread(() -> {
            try (BufferedReader logReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String logLine;
                while ((logLine = logReader.readLine()) != null) {
                    System.out.println("[Node-LOG] " + logLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Lettura JSON finale (stdout)
        StringBuilder output = new StringBuilder();
        try (BufferedReader jsonReader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String jsonLine;
            while ((jsonLine = jsonReader.readLine()) != null) {
                output.append(jsonLine);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Errore: processo Node.js terminato con codice " + exitCode);
            return;
        }

        // Parse JSON con Gson
        Gson gson = new Gson();
        System.out.println(output.toString());
        JsonObject json = gson.fromJson(output.toString(), JsonObject.class);

        if (json.has("error") && !json.get("error").isJsonNull() && !json.get("error").getAsString().isEmpty()) {
            System.err.println("Errore dallo script JS: " + json.get("error").getAsString());
        } else {
            // Depositi
            System.out.println("=== Depositi ===");
            JsonArray deposits = json.has("deposits") ? json.getAsJsonArray("deposits") : new JsonArray();
            //convertDepositi(deposits,"Binance");
            Importazioni.inserisciListaMovimentisuMappaCryptoWallet(convertDepositi(deposits,"Binance"));
            for (JsonElement d : deposits) {
                System.out.println(d.toString());
            }

            // Prelievi
            System.out.println("=== Prelievi ===");
            JsonArray withdrawals = json.has("withdrawals") ? json.getAsJsonArray("withdrawals") : new JsonArray();
            Importazioni.inserisciListaMovimentisuMappaCryptoWallet(convertPrelievi(withdrawals,"Binance"));
            for (JsonElement w : withdrawals) {
                System.out.println(w.toString());
            }

            // Trades
            System.out.println("=== Trades ===");
            JsonArray trades = json.has("trades") ? json.getAsJsonArray("trades") : new JsonArray();
            Importazioni.inserisciListaMovimentisuMappaCryptoWallet(convertTrades(trades,"Binance"));
            for (JsonElement t : trades) {
                System.out.println(t.toString());
            }

            // Conversioni
            System.out.println("=== Conversioni ===");
            JsonArray conversions = json.has("conversions") ? json.getAsJsonArray("conversions") : new JsonArray();
            for (JsonElement c2 : conversions) {
                System.out.println(c2.toString());
            }

            // Savings / Earn
            System.out.println("=== Savings ===");
            JsonArray savings = json.has("savings") ? json.getAsJsonArray("savings") : new JsonArray();
            for (JsonElement s : savings) {
                System.out.println(s.toString());
            }

            // Staking
            System.out.println("=== Staking ===");
            JsonArray staking = json.has("staking") ? json.getAsJsonArray("staking") : new JsonArray();
            for (JsonElement s2 : staking) {
                System.out.println(s2.toString());
            }
            
            // Staking
            System.out.println("=== earnFlexible ===");
            JsonArray earnFlexible = json.has("earnFlexible") ? json.getAsJsonArray("earnFlexible") : new JsonArray();
            Importazioni.inserisciListaMovimentisuMappaCryptoWallet(convertEarn(earnFlexible,"Binance"));
            for (JsonElement s2 : earnFlexible) {
                System.out.println(s2.toString());
            }
            
            // Staking
            System.out.println("=== earnLocked ===");
            JsonArray earnLocked = json.has("Binance_EarnLocked") ? json.getAsJsonArray("Binance_EarnLocked") : new JsonArray();
            Importazioni.inserisciListaMovimentisuMappaCryptoWallet(convertEarn(earnLocked,"Binance"));
            for (JsonElement s2 : earnLocked) {
                System.out.println(s2.toString());
            }
            
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}



    
public static List<String[]> convertDepositi(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.has("insertTime") ? o1.get("insertTime").getAsString() : o1.get("completeTime").getAsString()
            );
            long t2 = Long.parseLong(
                o2.has("insertTime") ? o2.get("insertTime").getAsString() : o2.get("completeTime").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("coin", "");
            String amount = obj.optString("amount", "");
            String network = obj.optString("network", "");
            String txId = obj.optString("txId", "");
            String address = obj.optString("address", "");
            String insertTime = obj.optString("insertTime", "completeTime");
            //String completeTime = obj.optString("completeTime", insertTime);

            long time = Long.parseLong(insertTime);
            String data = OperazioniSuDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            
            // Tipo moneta: se c'è l'address --> Crypto
            // se non c'è l'address --> FIAT solo se coin = EUR o USD
            String tipoMoneta;
            if (!address.isEmpty()) {
                tipoMoneta = "Crypto";
            } else {
                if (coin.equalsIgnoreCase("EUR") || coin.equalsIgnoreCase("USD")) {
                    tipoMoneta = "FIAT";
                } else {
                    tipoMoneta = "Crypto";
                }
            }

            Moneta Mon=new Moneta();
            Mon.Moneta=coin;
            Mon.Tipo=tipoMoneta;
            Mon.Qta=amount;
            // Calcolo prezzo transazione - qui lo lasciamo vuoto oppure 0
            Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, time, null, true, 2, null);

            String[] RT = new String[Importazioni.ColonneTabella];
            RT[1] = dataa;                                               // Data e ora
            RT[2] = i + " di " + totMov;                                 // Numero movimenti
            RT[3] = Exchange;                                            // Exchange
            RT[4] = "Principale";                                        // Wallet
            RT[7] = "";                                                  // Causale originale (vuoto)
            if (tipoMoneta.equals("FIAT"))
            {
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_DF"; // TrasID
                RT[5]="DEPOSITO FIAT";
            }else 
                {
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_DC"; // TrasID
                RT[5]="DEPOSITO CRYPTO";
                }
            // Deposito → moneta entrante
            RT[6]  = "-> " + Mon.Moneta;                                 // Dettaglio Movimento
            RT[8]  = "";
            RT[9]  = "";
            RT[10] = "";
            RT[11] = Mon.Moneta;                                         // Moneta Acq/Ricevuta
            RT[12] = Mon.Tipo;                                           // Tipo Moneta Acq.
            RT[13] = Mon.Qta;                                            // Quantità Acq.
        
            RT[14] = "";                                                 // Valore Mercato originale
            RT[15] = Mon.Prezzo;                                         // Valore in EURO (qui 0)
            RT[16] = ""; RT[17] = ""; RT[18] = ""; RT[19] = ""; RT[20] = ""; RT[21] = "";
            RT[22] = "A";                                               // Auto
            RT[23] = "";                                                 // [DEFI] Blocco Transazione
            RT[24] = txId;                                               // [DEFI] Hash Transazione
            RT[25] = "";                                                 // Nome Token Uscita
            RT[26] = "";                                                 // Address Token Uscita
            RT[27] = "";                                                 // Nome Token Entrata
            RT[28] = "";                                                 // Address Token Entrata
            RT[29] = insertTime;                                         // Timestamp
            RT[30] = "";                                                 // Address controparte
            RT[31] = "";                                                 // Data fine trasferimento
            RT[32] = "";                                                 // Movimento ha prezzo
            RT[33] = "";                                                 // Movimento genera plusvalenza
            RT[34] = network;                                            // Rete
            RT[35] = ""; RT[36] = ""; 
            RT[37] = address; 
            RT[38] = ""; RT[39] = "";

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
        }

        return lista;
    }    
    
   public static List<String[]> convertPrelievi(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.has("insertTime") ? o1.get("insertTime").getAsString() : o1.get("completeTime").getAsString()
            );
            long t2 = Long.parseLong(
                o2.has("insertTime") ? o2.get("insertTime").getAsString() : o2.get("completeTime").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("coin", "");
            String amount = obj.optString("amount", "");
            String network = obj.optString("network", "");
            String txId = obj.optString("txId", "");
            String address = obj.optString("address", "");
            //String transferType = obj.optString("transferType", "0"); // 0 deposito, 1 prelievo
            String fee = obj.optString("transactionFee", "0"); // 0 deposito, 1 prelievo
            String insertTime = obj.optString("insertTime", "completeTime");
            //String completeTime = obj.optString("completeTime", insertTime);

            long time = Long.parseLong(insertTime);
            String data = OperazioniSuDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            
            // Tipo moneta: se c'è l'address --> Crypto
            // se non c'è l'address --> FIAT solo se coin = EUR o USD
            String tipoMoneta;
            if (!address.isEmpty()) {
                tipoMoneta = "Crypto";
            } else {
                if (coin.equalsIgnoreCase("EUR") || coin.equalsIgnoreCase("USD")) {
                    tipoMoneta = "FIAT";
                } else {
                    tipoMoneta = "Crypto";
                }
            }

            Moneta Mon=new Moneta();
            Mon.Moneta=coin;
            Mon.Tipo=tipoMoneta;
            Mon.Qta=new BigDecimal(amount).abs().multiply(new BigDecimal(-1)).toPlainString();
            // Calcolo prezzo transazione - qui lo lasciamo vuoto oppure 0
            Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, time, null, true, 2, null);

            String[] RT = new String[Importazioni.ColonneTabella];
            RT[1] = dataa;                                               // Data e ora
            RT[2] = i + " di " + totMov;                                 // Numero movimenti
            RT[3] = Exchange;                                            // Exchange
            RT[4] = "Principale";                                        // Wallet
            RT[7] = "";                                                  // Causale originale (vuoto)
            // Prelievo → moneta uscente
            if (tipoMoneta.equals("FIAT"))
            {    
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_PF"; // TrasID
                RT[5]="PRELIEVO FIAT";
            }
            else 
            {    
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_PC"; // TrasID
                RT[5]="PRELIEVO CRYPTO";
            }
            RT[6]  = Mon.Moneta + " ->";                                       // Dettaglio Movimento
            RT[8]  = Mon.Moneta;                                               // Moneta Venduta/Trasferita (vuoto per deposito)
            RT[9]  = Mon.Tipo;                                         // Tipo Moneta Venduta
            RT[10] = Mon.Qta;                                             // Quantità Venduta
            RT[11] = "";
            RT[12] = "";
            RT[13] = "";
            RT[14] = "";                                                 // Valore Mercato originale
            RT[15] = Mon.Prezzo;                                         // Valore in EURO (qui 0)
            RT[16] = ""; RT[17] = ""; RT[18] = ""; RT[19] = ""; RT[20] = ""; RT[21] = "";
            RT[22] = "A";                                               // Auto
            RT[23] = "";                                                 // [DEFI] Blocco Transazione
            RT[24] = txId;                                               // [DEFI] Hash Transazione
            RT[25] = "";                                                 // Nome Token Uscita
            RT[26] = "";                                                 // Address Token Uscita
            RT[27] = "";                                                 // Nome Token Entrata
            RT[28] = "";                                                 // Address Token Entrata
            RT[29] = insertTime;                                         // Timestamp
            RT[30] = "";                                                 // Address controparte
            RT[31] = "";                                                 // Data fine trasferimento
            RT[32] = "";                                                 // Movimento ha prezzo
            RT[33] = "";                                                 // Movimento genera plusvalenza
            RT[34] = network;                                            // Rete
            RT[35] = ""; RT[36] = ""; 
            RT[37] = address; 
            RT[38] = ""; RT[39] = "";

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
            
            
            //SECONDA PARTE RELATIVA ALLE FEE
            Mon.Qta=new BigDecimal(fee).abs().multiply(new BigDecimal(-1)).toPlainString();
            Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, time, null, true, 2, null);
            
            RT = new String[Importazioni.ColonneTabella];
            RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + "2" + "_CM"; // TrasID
            RT[1] = dataa;                                               // Data e ora
            RT[2] = i + " di " + totMov;                                 // Numero movimenti
            RT[3] = Exchange;                                            // Exchange
            RT[4] = "Principale";                                        // Wallet
            RT[5] = "COMMISSIONI";
            RT[7] = "";                                                  // Causale originale (vuoto)
            RT[6]  = Mon.Moneta + " ->";                                       // Dettaglio Movimento
            RT[8]  = Mon.Moneta;                                               // Moneta Venduta/Trasferita (vuoto per deposito)
            RT[9]  = Mon.Tipo;                                         // Tipo Moneta Venduta
            RT[10] = Mon.Qta;                                             // Quantità Venduta
            RT[11] = "";
            RT[12] = "";
            RT[13] = "";
            RT[14] = "";                                                 // Valore Mercato originale
            RT[15] = Mon.Prezzo;                                         // Valore in EURO (qui 0)
            RT[16] = ""; RT[17] = ""; RT[18] = ""; RT[19] = ""; RT[20] = ""; RT[21] = "";
            RT[22] = "A";                                               // Auto
            RT[23] = "";                                                 // [DEFI] Blocco Transazione
            RT[24] = txId;                                               // [DEFI] Hash Transazione
            RT[25] = "";                                                 // Nome Token Uscita
            RT[26] = "";                                                 // Address Token Uscita
            RT[27] = "";                                                 // Nome Token Entrata
            RT[28] = "";                                                 // Address Token Entrata
            RT[29] = insertTime;                                         // Timestamp
            RT[30] = "";                                                 // Address controparte
            RT[31] = "";                                                 // Data fine trasferimento
            RT[32] = "";                                                 // Movimento ha prezzo
            RT[33] = "";                                                 // Movimento genera plusvalenza
            RT[34] = network;                                            // Rete
            RT[35] = ""; RT[36] = ""; 
            RT[37] = address; 
            RT[38] = ""; RT[39] = "";

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
        }

        return lista;
    }     
    
   public static List<String[]> convertTrades(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(o1.get("timestamp").getAsString());
            long t2 = Long.parseLong(o2.get("timestamp").getAsString());
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            JSONObject obj = new JSONObject(el.toString());
            Moneta mu=new Moneta();
            Moneta me=new Moneta();
            
            String verso = obj.optString("side", "");
            String Simboli[] = obj.optString("symbol", "").split("/");
            if (verso.equalsIgnoreCase("sell"))
            {
                mu.Moneta=Simboli[0];
                mu.Qta=obj.getJSONObject("info").optString("qty", "");
                mu.Tipo = (mu.Moneta.equalsIgnoreCase("EUR") || mu.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
                me.Moneta=Simboli[1];
                me.Qta=obj.getJSONObject("info").optString("quoteQty", "");
                me.Tipo = (me.Moneta.equalsIgnoreCase("EUR") || me.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
            }
            else
            {
                mu.Moneta=Simboli[1];
                mu.Qta=obj.getJSONObject("info").optString("quoteQty", "");
                mu.Tipo = (mu.Moneta.equalsIgnoreCase("EUR") || mu.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
                me.Moneta=Simboli[0];
                me.Qta=obj.getJSONObject("info").optString("qty", "");
                me.Tipo = (me.Moneta.equalsIgnoreCase("EUR") || me.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
            }
            
            mu.Qta=new BigDecimal(mu.Qta).abs().stripTrailingZeros().multiply(new BigDecimal(-1)).toPlainString();
            me.Qta=new BigDecimal(me.Qta).abs().stripTrailingZeros().toPlainString();
            
            Moneta mc=new Moneta();
            mc.Moneta = obj.getJSONObject("fee").optString("currency", "");
            mc.Qta = new BigDecimal(obj.getJSONObject("fee").optString("cost", "")).stripTrailingZeros().toPlainString();
            mc.Tipo = (mc.Moneta.equalsIgnoreCase("EUR") || mc.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
            String Time = obj.optString("timestamp", "");
            //String completeTime = obj.optString("completeTime", insertTime);

            long time = Long.parseLong(Time);
            String data = OperazioniSuDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            String PrezzoT = Prezzi.DammiPrezzoTransazione(mu, me, time, null, true, 2, null);

            String[] RT = new String[Importazioni.ColonneTabella];
            RT[1] = dataa;                                               // Data e ora
            RT[2] = i + " di " + totMov;                                 // Numero movimenti
            RT[3] = Exchange;                                            // Exchange
            RT[4] = "Principale";                                        // Wallet
            RT[7] = "";                                                  // Causale originale (vuoto)
            // Prelievo → moneta uscente
            
            //Scambio FIAT
            if (mu.Tipo.equalsIgnoreCase("FIAT") && me.Tipo.equalsIgnoreCase("FIAT"))
            {    
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_SF"; // TrasID
                RT[5]="SCAMBIO FIAT";
            }
            //Acquisto Crypto
            else if (mu.Tipo.equalsIgnoreCase("FIAT") && !me.Tipo.equalsIgnoreCase("FIAT"))
            {    
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_AC"; // TrasID
                RT[5]="ACQUISTO CRYPTO";
            }
            //Vendita Crypto
            else if (!mu.Tipo.equalsIgnoreCase("FIAT") && me.Tipo.equalsIgnoreCase("FIAT"))
            {    
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_VC"; // TrasID
                RT[5]="VENDITA CRYPTO";
            }
            //Scambio Crypto
            else {
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_SC"; // TrasID
                RT[5]="SCAMBIO CRYPTO";
            }
            RT[6]  = mu.Moneta + " -> "+me.Moneta;                                       // Dettaglio Movimento
            RT[8]  = mu.Moneta;                                               // Moneta Venduta/Trasferita (vuoto per deposito)
            RT[9]  = mu.Tipo;                                         // Tipo Moneta Venduta
            RT[10] = mu.Qta;                                             // Quantità Venduta
            RT[11] = me.Moneta;
            RT[12] = me.Tipo;
            RT[13] = me.Qta;
            RT[14] = "";                                                 // Valore Mercato originale
            RT[15] = PrezzoT;                                          // Valore in EURO (qui 0)
            RT[16] = ""; RT[17] = ""; RT[18] = ""; RT[19] = ""; RT[20] = ""; RT[21] = "";
            RT[22] = "A";                                               // Auto
            RT[23] = "";                                                 // [DEFI] Blocco Transazione
            RT[24] = "";                                               // [DEFI] Hash Transazione
            RT[25] = "";                                                 // Nome Token Uscita
            RT[26] = "";                                                 // Address Token Uscita
            RT[27] = "";                                                 // Nome Token Entrata
            RT[28] = "";                                                 // Address Token Entrata
            RT[29] = Time;                                                 // Timestamp
            RT[30] = "";                                                 // Address controparte
            RT[31] = "";                                                 // Data fine trasferimento
            RT[32] = "";                                                 // Movimento ha prezzo
            RT[33] = "";                                                 // Movimento genera plusvalenza
            RT[34] = "";                                            // Rete
            RT[35] = ""; RT[36] = ""; 
            RT[37] = ""; 
            RT[38] = ""; RT[39] = "";

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
            
            
            //SECONDA PARTE RELATIVA ALLE FEE
            mc.Qta=new BigDecimal(mc.Qta).abs().multiply(new BigDecimal(-1)).toPlainString();
            String PrezzoC = Prezzi.DammiPrezzoTransazione(mc, null, time, null, true, 2, null);
            
            RT = new String[Importazioni.ColonneTabella];
            RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + "2" + "_CM"; // TrasID
            RT[1] = dataa;                                               // Data e ora
            RT[2] = i + " di " + totMov;                                 // Numero movimenti
            RT[3] = Exchange;                                            // Exchange
            RT[4] = "Principale";                                        // Wallet
            RT[5] = "COMMISSIONI";
            RT[7] = "";                                                  // Causale originale (vuoto)
            RT[6]  = mc.Moneta + " ->";                                       // Dettaglio Movimento
            RT[8]  = mc.Moneta;                                               // Moneta Venduta/Trasferita (vuoto per deposito)
            RT[9]  = mc.Tipo;                                         // Tipo Moneta Venduta
            RT[10] = mc.Qta;                                             // Quantità Venduta
            RT[11] = "";
            RT[12] = "";
            RT[13] = "";
            RT[14] = "";                                                 // Valore Mercato originale
            RT[15] = PrezzoC;                                         // Valore in EURO (qui 0)
            RT[16] = ""; RT[17] = ""; RT[18] = ""; RT[19] = ""; RT[20] = ""; RT[21] = "";
            RT[22] = "A";                                               // Auto
            RT[23] = "";                                                 // [DEFI] Blocco Transazione
            RT[24] = "";                                               // [DEFI] Hash Transazione
            RT[25] = "";                                                 // Nome Token Uscita
            RT[26] = "";                                                 // Address Token Uscita
            RT[27] = "";                                                 // Nome Token Entrata
            RT[28] = "";                                                 // Address Token Entrata
            RT[29] = Time;                                         // Timestamp
            RT[30] = "";                                                 // Address controparte
            RT[31] = "";                                                 // Data fine trasferimento
            RT[32] = "";                                                 // Movimento ha prezzo
            RT[33] = "";                                                 // Movimento genera plusvalenza
            RT[34] = "";                                            // Rete
            RT[35] = ""; RT[36] = ""; 
            RT[37] = ""; 
            RT[38] = ""; RT[39] = "";

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
        }

        return lista;
    }     
   
   
    
   public static List<String[]> convertEarn(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.get("time").getAsString()
            );
            long t2 = Long.parseLong(
                o2.get("time").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("asset", "");
            String amount = obj.optString("amount", "");
            amount = obj.optString("rewards", amount);
            String tipo = obj.optString("type", "");
            String insertTime = obj.optString("time", "");


            long time = Long.parseLong(insertTime);
            String data = OperazioniSuDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            
            // Tipo moneta: se c'è l'address --> Crypto
            // se non c'è l'address --> FIAT solo se coin = EUR o USD
            String tipoMoneta;
                if (coin.equalsIgnoreCase("EUR") || coin.equalsIgnoreCase("USD")) {
                    tipoMoneta = "FIAT";
                } else {
                    tipoMoneta = "Crypto";
                }
            

            Moneta Mon=new Moneta();
            Mon.Moneta=coin;
            Mon.Tipo=tipoMoneta;
            Mon.Qta=amount;
            // Calcolo prezzo transazione - qui lo lasciamo vuoto oppure 0
            Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, time, null, true, 2, null);

            String[] RT = new String[Importazioni.ColonneTabella];
            RT[1] = dataa;                                               // Data e ora
            RT[2] = i + " di " + totMov;                                 // Numero movimenti
            RT[3] = Exchange;                                            // Exchange
            RT[4] = "Principale";                                        // Wallet
            RT[7] = "";                                                  // Causale originale (vuoto)
            if (tipoMoneta.equals("FIAT"))
            {
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_DF"; // TrasID
                RT[5]="DEPOSITO FIAT";
            }else 
                {
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_RW"; // TrasID
                RT[5]="EARN";
                }
            // Deposito → moneta entrante
            RT[6]  = "-> " + Mon.Moneta;                                 // Dettaglio Movimento
            RT[8]  = "";
            RT[9]  = "";
            RT[10] = "";
            RT[11] = Mon.Moneta;                                         // Moneta Acq/Ricevuta
            RT[12] = Mon.Tipo;                                           // Tipo Moneta Acq.
            RT[13] = Mon.Qta;                                            // Quantità Acq.
        
            RT[14] = "";                                                 // Valore Mercato originale
            RT[15] = Mon.Prezzo;                                         // Valore in EURO (qui 0)
            RT[16] = ""; RT[17] = ""; RT[18] = ""; RT[19] = ""; RT[20] = ""; RT[21] = "";
            RT[22] = "A";                                               // Auto
            RT[23] = "";                                                 // [DEFI] Blocco Transazione
            RT[24] = "";                                               // [DEFI] Hash Transazione
            RT[25] = "";                                                 // Nome Token Uscita
            RT[26] = "";                                                 // Address Token Uscita
            RT[27] = "";                                                 // Nome Token Entrata
            RT[28] = "";                                                 // Address Token Entrata
            RT[29] = insertTime;                                         // Timestamp
            RT[30] = "";                                                 // Address controparte
            RT[31] = "";                                                 // Data fine trasferimento
            RT[32] = "";                                                 // Movimento ha prezzo
            RT[33] = "";                                                 // Movimento genera plusvalenza
            RT[34] = "";                                            // Rete
            RT[35] = ""; RT[36] = ""; 
            RT[37] = ""; 
            RT[38] = ""; RT[39] = "";

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
        }

        return lista;
    }    
   
   
   

    public static void main(String[] args) {

    }
}

