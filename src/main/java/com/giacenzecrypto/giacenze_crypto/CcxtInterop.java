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
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.awt.Component;
import java.awt.Cursor;
import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.json.JSONObject;

public class CcxtInterop {
    
    private static final String NODE_VERSION = "v24.7.0";
    private static final Path NODE_DIR = Paths.get(Statiche.getWorkingDirectory()+"tools", "node").toAbsolutePath().normalize();;
    

    
    public static void ensureNodeInstalled() throws IOException {
        

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
        
        //Verifico se è presente una specifica versione di node, qualora non lo sia scarico la nuova
        if (Files.exists(Paths.get(NODE_DIR.toString()+"/node-" + NODE_VERSION + "-" + platform))) {
            System.out.println("✅ Node.js già presente");
            return;
        }

        Path downloadPath = Paths.get(Statiche.getWorkingDirectory()+"tools", filename);
        Files.createDirectories(downloadPath.getParent());

        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, downloadPath, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Scaricato: " + filename);
        System.out.println("Node DIR = "+NODE_DIR);
        extractArchive(downloadPath, NODE_DIR, extension);
        System.out.println("Estratto Node.js");

        // su Windows, dentro la cartella estratta ci sarà node.exe + npm.cmd
        // su Linux/macOS, sono in /bin
    }
    

    
    
    public static void installCcxt() throws IOException, InterruptedException {
        
        
        Path nodePath = getNodeExePath();
        System.out.println("nodePath="+nodePath);

        // Non reindirizziamo stderr su stdout
        // builder.redirectErrorStream(true);
        // Calcola la cartella base di Node in modo multipiattaforma
    /*    Path nodeBaseDir = nodePath.getParent(); // es: .../node-vXX-PLATFORM[/bin]
        if (!nodeBaseDir.getFileName().toString().equals("bin")) {
            // Se siamo su Windows, node.exe sta direttamente in base dir, altrimenti sotto bin
            nodeBaseDir = nodeBaseDir.getParent();
        }      */
        
    Path nodeModulesDir = NODE_DIR.resolve("node_modules");
    //Path nodeModulesDir = nodeBaseDir.resolve("node_modules").toAbsolutePath();
    Path ccxtDir = nodeModulesDir.resolve("ccxt");

    if (Files.exists(ccxtDir) && Files.isDirectory(ccxtDir)) {
        System.out.println("CCXT già installato in: " + ccxtDir);
        return;
    }    
    
    Path npmPath = getNpmPath();
    System.out.println("npmPath="+npmPath);
    

    System.out.println("Installo ccxt...");
    ProcessBuilder builder = new ProcessBuilder(npmPath.toString(), "install", "ccxt");
    System.out.println(Statiche.getWorkingDirectory() + "tools/node");
    builder.directory(NODE_DIR.toFile());
    //builder.directory(nodeModulesDir.toFile());
    //builder.directory(new File(Statiche.getWorkingDirectory() + "tools/node"));  // directory di lavoro
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

 public static void installModuleNode(String Modulo) throws IOException, InterruptedException {
    //Per il momento mi servono installati ccxt,express e cors    
        
        Path nodePath = getNodeExePath();
        System.out.println("nodePath="+nodePath);
        
    Path nodeModulesDir = NODE_DIR.resolve("node_modules");
    //Path nodeModulesDir = nodeBaseDir.resolve("node_modules").toAbsolutePath();
    Path ModuloDir = nodeModulesDir.resolve(Modulo);

    if (Files.exists(ModuloDir) && Files.isDirectory(ModuloDir)) {
        System.out.println("express già installato in: " + ModuloDir);
        return;
    }    
    
    Path npmPath = getNpmPath();
    System.out.println("npmPath="+npmPath);
    

    System.out.println("Installo "+Modulo+"...");
    ProcessBuilder builder = new ProcessBuilder(npmPath.toString(), "install", Modulo);
    System.out.println(Statiche.getWorkingDirectory() + "tools/node");
    builder.directory(NODE_DIR.toFile());
    //builder.directory(nodeModulesDir.toFile());
    //builder.directory(new File(Statiche.getWorkingDirectory() + "tools/node"));  // directory di lavoro
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
    if (exitCode != 0) throw new RuntimeException("npm install "+Modulo+" fallito");
    System.out.println(Modulo+" installato con successo");
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
          /*  new ProcessBuilder("tar", "-xf", archive.toString(), "-C", targetDir.getParent().toString())
                .inheritIO().start();*/
          // Creare la cartella targetDir se non esiste
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        //  System.out.println("AAA "+targetDir.toString());
            new ProcessBuilder("tar", "-xf", archive.toString(), "-C", targetDir.toString())
            .inheritIO().start();
        }
    }
    
    
    public static void fetchMovimentiConBar(String exchangeId, String apiKey, String secret, long startDate,String Tokens,Component c) {
             // TODO add your handling code here:
        //CcxtInterop a = new CcxtInterop();
        c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Download progress = new Download();
        progress.setIndeterminate(true);
        progress.SetLabel("Scaricamento da API in corso...");
        //progress.RipristinaStdout();
        //progress.MostraProgressAttesa("Export in Excel", "Esportazione in corso...");
        progress.setLocationRelativeTo(c);

        // Esegui l'export in background
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
        @Override
        protected Void doInBackground() throws Exception {
        try {
        System.out.println("Verifico Installazione di Node");
        ensureNodeInstalled();
        System.out.println("Verifico Installazione di CCXT");
        installCcxt();
        System.out.println("Eseguo la chiamata");
        
        fetchMovimenti(exchangeId, apiKey, secret,startDate,Tokens,progress,c);
        } catch (IOException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(CDC_Grafica.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
        }
        
        @Override
        protected void done() {
        progress.dispose();
        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        };

        worker.execute();
        progress.setVisible(true);// Questo blocca finché done() non chiama dispose()
        
    }
    
    

    public static void fetchMovimenti(String exchangeId, String apiKey, String secret, long startDate,String Tokens,Download progress,Component c) {
       // Map<String, JsonObject> Mappa_Json = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<JsonObject> Jsons = new ArrayList<>();
        
        //BINACE TEST
        if (exchangeId.equalsIgnoreCase("Binancet")) {
            long inizioanno=Long.parseLong("1609465487000");
            //1 - RECUPERO TUTTI I MOVIMENTI TRANNE I TRADES
           // String estrazioni[] = new String[]{"depositi", "prelievi", "Binance_Conversioni", "Binance_EarnFlessibili", "Binance_EarnLocked"};
            String estrazioni[] = new String[]{"check_node_path"};
            for (String script : estrazioni) {
                JsonObject json = fetchMovimento(exchangeId, apiKey, secret, inizioanno, "", script);
                if (json != null) {
                    Jsons.add(json);
                }
            }

            
            //2 - RECUPERO I TOKEN COINVOLTI NELLE TRANSAZIONI
            //Adesso che ho scaricato tutti i movimenti recupero la lista dei movimenti nel formato standard di GiacenzeCrypto
            List<String[]> lista = getListaMovimenti(Jsons, exchangeId);

            //Adesso devo recuperare tutti i token movimentati per poter poi creare un array di token da passare per il recupero dei trades
            for (String[] riga : lista) {
                //Se il token non è presente nella lista lo aggiungo
                if (!riga[8].isBlank() && !Tokens.contains(riga[8])) {
                    Tokens = Tokens + "," + riga[8];
                }
                if (!riga[11].isBlank() && !Tokens.contains(riga[11])) {
                    Tokens = Tokens + "," + riga[11];
                }
            }
            
            DatabaseH2.Pers_ExchangeTokens_LeggiTokensExchange("Binance");
            
            //Recupero la lista dei token con le varie somme e prendo solo quelli che hanno una somma diversa da zero
            //solo su quelli vado a cercare i trades, infatti i token che vanno a zero molto probabilmente non sono stati scambiati oltre le varie conversions
            

            //3 - RECUPERO I TRADES DEI TOKEN COINVOLTI + QUELLI RICHIESTI IN ORIGINE
            //Importazioni.inserisciListaMovimentisuMappaCryptoWallet(
            //Recuperato la lista di token da richiedere procedo con il recupero dei trades
           // JsonObject json = fetchMovimento(exchangeId, apiKey, secret, startDate, Tokens, "Binance_Trades");
           // lista.addAll(getListaMovimento(json, exchangeId));

            //4 - IMPORTO TUTTO NEL DATABASE
            //Recuperati tutti i movimenti posso procedere all'aggiunta al database vera e propria
            if(!CDC_Grafica.InterrompiCiclo)
                Importazioni.inserisciListaMovimentisuMappaCryptoWallet(lista);
            //Solo se non ho premutol il tasto annulla, in quel caso non faccio nulla
            else{
                JOptionPane.showConfirmDialog(c, "Elaborazione interrotta dall'utente!",
                                "Attenzione", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null);
            }

        }
        
        //BINACE
        if (exchangeId.equalsIgnoreCase("Binance")) {
            
            //Intanto aggiungo in una lista univoca i tokens
            Set<String> setTokens = new HashSet<>();
            String tokens[]=Tokens.split(",");
            setTokens.addAll(Arrays.asList(tokens));
            
            //0 - RECUPERO TUTTI I TOKEN DI BINANCE CON GIACENZA DIVERSA DA ZERO
            //Recupero la lista dei token con le varie somme e prendo solo quelli che hanno una somma diversa da zero
            //solo su quelli vado a cercare i trades, infatti i token che vanno a zero molto probabilmente non sono stati scambiati oltre le varie conversions
            Map<String, Moneta> QtaCrypto = new TreeMap<>();//nel primo oggetto metto l'ID, come secondo oggetto metto la moneta con tutti i dati
            for (String[] movimento : CDC_Grafica.MappaCryptoWallet.values()) {
                if (movimento[3].trim().equalsIgnoreCase("Binance")) {
                    Moneta Monete[] = new Moneta[2];//in questo array metto la moneta in entrata e quellain uscita
                    //in paricolare la moneta in uscita nella posizione 0 e quella in entrata nella posizione 1
                    Monete[0] = new Moneta();
                    Monete[1] = new Moneta();
                    Monete[0].Moneta = movimento[8];
                    Monete[0].Tipo = movimento[9];
                    Monete[0].Qta = movimento[10];
                    Monete[1].Moneta = movimento[11];
                    Monete[1].Tipo = movimento[12];
                    Monete[1].Qta = movimento[13];
                    //questo ciclo for serve per recuperare le qta di tutte le monete
                    for (int a = 0; a < 2; a++) {
                        //ANALIZZO MOVIMENTI
                        if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo) != null) {
                            //Movimento già presente da implementare
                            Moneta M1 = QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo);
                            M1.Qta = new BigDecimal(M1.Qta)
                                    .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                        } else if (!Monete[a].Moneta.isBlank()) {
                            //Movimento Nuovo da inserire
                            Moneta M1 = new Moneta();
                            M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, "", Monete[a].Tipo);//il campo vuoto sarebbe risevato all'address che non mi serve in questo momento
                            QtaCrypto.put(Monete[a].Moneta + ";" + Monete[a].Tipo, M1);

                        }
                    }
                }
            }           
            //Adesso che ho le qta di tutte le monete le metto quelli che hanno qta zero in una lista separata da virgole e la do in pasto alla funzione che recupera i trades
            for(Moneta m:QtaCrypto.values()){
                if (BG(m.Qta).compareTo(BigDecimal.ZERO)!=0){
                    //Tokens=Tokens+","+m.Moneta;
                    setTokens.add(m.Moneta);
                }
            }
            
            //1 - RECUPERO TUTTI I MOVIMENTI TRANNE I TRADES
            progress.setTitle("Scaricamento dei dati di "+exchangeId+" tramite API");
            String estrazioni[] = new String[]{"depositi", "prelievi", "Binance_MovimentiFiat","Binance_Conversioni","Binance_ConversioniSmall", "Binance_AssetDividend","Binance_EarnFlessibili","Binance_EarnLocked"};
            int chiamate=estrazioni.length+1;
            int j=0;
            for (String script : estrazioni) {
                //Interrompo la funzione se ho premuto interrompi o se ho degli errori bloccanti sulla funzione
                if (CDC_Grafica.InterrompiCiclo||progress.ErroriNodeJS())
                {
                    JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                    return;
                }
                j++;
                progress.SetMessaggioAvanzamento("Comunicazione con endpoint "+j+" di "+chiamate+" in corso...");
                JsonObject json = fetchMovimento(exchangeId, apiKey, secret, startDate, "", script);
                if (json != null) {
                    Jsons.add(json);
                }
            }

            
            //2 - RECUPERO I TOKEN COINVOLTI NELLE TRANSAZIONI PER CUI LA LORO SOMMA SIA DIVERSA DA ZERO
            //Adesso che ho scaricato tutti i movimenti recupero la lista dei movimenti nel formato standard di GiacenzeCrypto
            List<String[]> lista = getListaMovimenti(Jsons, exchangeId);


            //Recupero la lista dei token con le varie somme e prendo solo quelli che hanno una somma diversa da zero
            //solo su quelli vado a cercare i trades, infatti i token che vanno a zero molto probabilmente non sono stati scambiati oltre le varie conversions
            QtaCrypto = new TreeMap<>();//nel primo oggetto metto l'ID, come secondo oggetto metto la moneta con tutti i dati
            for (String[] movimento : lista) {
                Moneta Monete[] = new Moneta[2];//in questo array metto la moneta in entrata e quellain uscita
                //in paricolare la moneta in uscita nella posizione 0 e quella in entrata nella posizione 1
                Monete[0] = new Moneta();
                Monete[1] = new Moneta();
                Monete[0].Moneta = movimento[8];
                Monete[0].Tipo = movimento[9];
                Monete[0].Qta = movimento[10];
                Monete[1].Moneta = movimento[11];
                Monete[1].Tipo = movimento[12];
                Monete[1].Qta = movimento[13];                
                //questo ciclo for serve per recuperare le qta di tutte le monete
                for (int a = 0; a < 2; a++) {
                    //ANALIZZO MOVIMENTI
                    if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo) != null) {
                        //Movimento già presente da implementare
                        Moneta M1 = QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo);
                        M1.Qta = new BigDecimal(M1.Qta)
                                .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                    } else if (!Monete[a].Moneta.isBlank()) {
                        //Movimento Nuovo da inserire
                        Moneta M1 = new Moneta();
                        M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, "", Monete[a].Tipo);//il campo vuoto sarebbe risevato all'address che non mi serve in questo momento
                        QtaCrypto.put(Monete[a].Moneta + ";" + Monete[a].Tipo, M1);

                    }
                }
            }
            
            //Adesso che ho le qta di tutte le monete le metto quelli che hanno qta zero in una lista separata da virgole e la do in pasto alla funzione che recupera i trades
            for(Moneta m:QtaCrypto.values()){
                if (BG(m.Qta).compareTo(BigDecimal.ZERO)!=0){
                    setTokens.add(m.Moneta);
                    //Tokens=Tokens+","+m.Moneta;
                }
            }
            //Come ultima cosa aggiungo i token Forzati manualmente alla lista
            List<String> lis=DatabaseH2.Pers_ExchangeTokens_LeggiTokensExchange("Binance");
            for (String l:lis){
                setTokens.add(l);
            }
            
            //Butto tutti i token nella stringa da passare allo script
            String tok="";
            for (String t:setTokens){
                tok=tok+","+t;
            }
            
            //Come ultima cosa aggiungo i token Forzati manualmente alla lista
            
            //3 - RECUPERO I TRADES DEI TOKEN COINVOLTI + QUELLI RICHIESTI IN ORIGINE
            //Importazioni.inserisciListaMovimentisuMappaCryptoWallet(
            //Recuperato la lista di token da richiedere procedo con il recupero dei trades
            j++;
            progress.SetMessaggioAvanzamento("Comunicazione con endpoint "+j+" di "+chiamate+" in corso...");
            JsonObject json = fetchMovimento(exchangeId, apiKey, secret, startDate, tok, "Binance_Trades");
            lista.addAll(getListaMovimento(json, exchangeId));
            if (CDC_Grafica.InterrompiCiclo||progress.ErroriNodeJS())
                {
                    JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                    return;
                }

            //4 - IMPORTO TUTTO NEL DATABASE
            //Recuperati tutti i movimenti posso procedere all'aggiunta al database vera e propria
            //Se non è andato tutto a buon fine non porto a termine l'importazione
            Importazioni.inserisciListaMovimentisuMappaCryptoWallet(lista);

        }
    }
    
    public static BigDecimal BG(String S){
        return new BigDecimal(S);
    }
    
    public static List<String[]> getListaMovimento(JsonObject json,String Exchange) {
             List<String[]> lista = new ArrayList<>();       
             // Depositi
            JsonArray deposits = json.has("deposits") ? json.getAsJsonArray("deposits") : new JsonArray();
            lista.addAll(convertDepositi(deposits,Exchange));
          /*  for (JsonElement d : deposits) {
                System.out.println(d.toString());
            }*/

            // Prelievi
            JsonArray withdrawals = json.has("withdrawals") ? json.getAsJsonArray("withdrawals") : new JsonArray();
            lista.addAll(convertPrelievi(withdrawals,Exchange));
           /* for (JsonElement w : withdrawals) {
                System.out.println(w.toString());
            }*/

            // Trades
            JsonArray trades = json.has("trades") ? json.getAsJsonArray("trades") : new JsonArray();
            lista.addAll(convertTrades(trades,Exchange));
           /* for (JsonElement t : trades) {
                System.out.println(t.toString());
            }*/

            // Conversioni
            JsonArray conversions = json.has("Binance_smallAssetConversions") ? json.getAsJsonArray("Binance_smallAssetConversions") : new JsonArray();
            lista.addAll(convertBinanceConversioniSmall(conversions,Exchange));
            JsonArray conversions2 = json.has("Binance_Convert") ? json.getAsJsonArray("Binance_Convert") : new JsonArray();
            lista.addAll(convertBinanceConversioni(conversions2,Exchange));
           /* for (JsonElement c2 : conversions) {
                System.out.println(c2.toString());
            }*/

           
           // Depositi/prelievi e acquisti/vendite FIAT
            JsonObject Binance_fiat = json.has("Binance_fiat") ? json.getAsJsonObject("Binance_fiat") : new JsonObject();
            lista.addAll(convertBinanceMovimentiFiat(Binance_fiat,Exchange));

            
            
            // Savings / Earn
            JsonArray savings = json.has("savings") ? json.getAsJsonArray("savings") : new JsonArray();
           /* for (JsonElement s : savings) {
                System.out.println(s.toString());
            }*/

            // Staking
            JsonArray staking = json.has("staking") ? json.getAsJsonArray("staking") : new JsonArray();
          /*  for (JsonElement s2 : staking) {
                System.out.println(s2.toString());
            }*/
            
            // Binance Earn Flessibile
            JsonArray earnFlexible = json.has("Binance_earnFlexible") ? json.getAsJsonArray("Binance_earnFlexible") : new JsonArray();
            lista.addAll(convertBinanceEarn(earnFlexible,Exchange));
          /*  for (JsonElement s2 : earnFlexible) {
                System.out.println(s2.toString());
            }*/
            
            // Binance Earn Bloccato
            JsonArray earnLocked = json.has("Binance_EarnLocked") ? json.getAsJsonArray("Binance_EarnLocked") : new JsonArray();
            lista.addAll(convertBinanceEarn(earnLocked,Exchange));
            /*for (JsonElement s2 : earnLocked) {
                System.out.println(s2.toString());
            }*/
            
            // Binance Earn Bloccato
            JsonArray rewards = json.has("Binance_Rewards") ? json.getAsJsonArray("Binance_Rewards") : new JsonArray();
            lista.addAll(convertBinanceRewards(rewards,Exchange));
            /*for (JsonElement s2 : earnLocked) {
                System.out.println(s2.toString());
            }*/
            

            return lista;
    }
    
    public static List<String[]> getListaMovimenti(List<JsonObject> Jsons,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        for(JsonObject json:Jsons){
            lista.addAll(getListaMovimento(json,Exchange));
        }
       return lista; 
    }
    
    public static JsonObject fetchMovimento(String exchangeId, String apiKey, String secret, long startDate,String Tokens,String script) {
    try {
        
        
        
        Path nodePath = getNodeExePath();
        Path scriptPath = Paths.get(Statiche.getPathRisorse()
                + "Scripts/"
                + script
                + ".js");

        if (!Files.exists(nodePath)) {
            System.err.println("Errore: node non trovato a " + nodePath.toAbsolutePath());
            return null;
        }
        if (!Files.exists(scriptPath)) {
            System.err.println("Errore: script JS non trovato a " + scriptPath.toAbsolutePath());
            return null;
        }

        System.out.println("Eseguo script : "+script+".js");
        ProcessBuilder builder = new ProcessBuilder(
                nodePath.toString(),
                scriptPath.toAbsolutePath().toString(),
                exchangeId.toLowerCase(), apiKey, secret, String.valueOf(startDate),Tokens
        );
        builder.directory(scriptPath.getParent().toFile());
        // Non reindirizziamo stderr su stdout
        // builder.redirectErrorStream(true);
        // Calcola la cartella base di Node in modo multipiattaforma
      /*  Path nodeBaseDir = nodePath.getParent(); // es: .../node-vXX-PLATFORM[/bin]
        if (!nodeBaseDir.getFileName().toString().equals("bin")) {
            // Se siamo su Windows, node.exe sta direttamente in base dir, altrimenti sotto bin
            nodeBaseDir = nodeBaseDir.getParent();
        }
*/
        //Path nodeModulesPath = nodeBaseDir.resolve("node_modules").toAbsolutePath();
        Path nodeModulesPath = NODE_DIR.resolve("node_modules").toAbsolutePath();
        
        Map<String, String> env = builder.environment();

        // Aggiungi node_modules a NODE_PATH (se esiste già, concatena)
        String existingNodePath = env.get("NODE_PATH");
        //System.out.println("existingNodePath : "+existingNodePath);
        String newNodePath = nodeModulesPath.toString();
        //String newNodePath = NODE_DIR.toString();
        //System.out.println("newNodePath : "+newNodePath);
        if (existingNodePath != null && !existingNodePath.isEmpty()) {
            newNodePath += File.pathSeparator + existingNodePath;
        }
        env.put("NODE_PATH", newNodePath);
        System.out.println("newNodePath : "+newNodePath);

        Process process = builder.start();

        // Thread per log (stderr)
        new Thread(() -> {
            try (BufferedReader logReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String logLine;
                while ((logLine = logReader.readLine()) != null) {
                    if(CDC_Grafica.InterrompiCiclo)
                    {
                        System.out.println("Premuto tasto INTERROMPI, blocco l'esecuzione dello script");
                        process.destroy();
                        return;
                    }
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
            return null;
        }

        // Parse JSON con Gson
        Gson gson = new Gson();
        System.out.println(output.toString());
        JsonObject json = gson.fromJson(output.toString(), JsonObject.class);

        if (json.has("error") && !json.get("error").isJsonNull() && !json.get("error").getAsString().isEmpty()) {
            System.err.println("Errore dallo script JS: " + json.get("error").getAsString());
            return null;
        } else {
            return json;
                     
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return null;
}

public static void recuperPrezzi(String Symbol,long Since,Long Until) throws SQLException {

        
        //Lista degli exchange a cui richiedere il prezzo della cripto
        String exchanges="binance,cryptocom,bybit,okx,coinbase,bitstamp,kucoin";
        
        Path nodePath = getNodeExePath();
        Path scriptPath = Paths.get(Statiche.getPathRisorse()
                + "Scripts/"
                + "Historical_Multi_Eur"
                + ".js");

        if (!Files.exists(nodePath)) {
            System.err.println("Errore: node non trovato a " + nodePath.toAbsolutePath());
            return;
        }
        if (!Files.exists(scriptPath)) {
            System.err.println("Errore: script JS non trovato a " + scriptPath.toAbsolutePath());
            return;
        }

        System.out.println("Eseguo script : Historical_Multi_Eur.js");
         // Parametri CLI da passare allo script
        List<String> command = new ArrayList<>();
        command.add(nodePath.toString());
        command.add(scriptPath.toAbsolutePath().toString());
        command.add("--since");
        command.add(String.valueOf(Since));
        command.add("--until");
        command.add(String.valueOf(Until));
        command.add("--exchanges");
        command.add(exchanges);
        command.add("--symbol");
        command.add(Symbol);
        command.add("--timeframe");
        command.add("1m");
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(scriptPath.getParent().toFile());
        Path nodeModulesPath = NODE_DIR.resolve("node_modules").toAbsolutePath();
        Map<String, String> env = pb.environment();
        // Aggiungi node_modules a NODE_PATH (se esiste già, concatena)
        String existingNodePath = env.get("NODE_PATH");
        String newNodePath = nodeModulesPath.toString();
        if (existingNodePath != null && !existingNodePath.isEmpty()) {
            newNodePath += File.pathSeparator + existingNodePath;
        }
        env.put("NODE_PATH", newNodePath);
        pb.redirectErrorStream(true); // unisce stdout + stderr
        
         try {
            Process process = pb.start();

            // Leggi l'output dello script (JSON stampato da console.log)
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Attendi che il processo finisca
            int exitCode = process.waitFor();
            //System.out.println("Exit code: " + exitCode);
            if (exitCode != 0) {
                System.err.println("Script Node fallito. Exit code: " + exitCode);
                System.err.println(output);
                return;
            }
            
            // Parse JSON con Gson in modo sicuro (manteniamo precisione per i long)
        Gson gson = new Gson();
        JsonElement rootEl = JsonParser.parseString(output.toString());
        if (!rootEl.isJsonArray()) {
            System.err.println("Output non è un array JSON valido.");
            return;
        }
        JsonArray rootArr = rootEl.getAsJsonArray();

        // Connessione H2 (persistente su file nella home: jdbc:h2:~/pricesdb)
        //String jdbc = "jdbc:h2:~/pricesdb;AUTO_SERVER=TRUE";
        //try (Connection conn = DriverManager.getConnection(jdbc, "sa", "")) {


            // MERGE (upsert) per evitare errori se la riga esiste già
            // Nota: H2 supporta MERGE ... KEY (col1, col2, ...)
            String mergeSql = "MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo,rete,address) KEY (timestamp, exchange, symbol,rete,address) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(mergeSql)) {

                for (JsonElement el : rootArr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();

                    if (!obj.has("timestamp")) continue;
                    long ts = obj.get("timestamp").getAsLong();

                    JsonObject pricesObj = obj.has("prices") && obj.get("prices").isJsonObject()
                            ? obj.getAsJsonObject("prices")
                            : null;
                    if (pricesObj == null) continue;

                    for (Map.Entry<String, JsonElement> entry : pricesObj.entrySet()) {
                        String exchange = entry.getKey();
                        JsonElement valEl = entry.getValue();
                        if (valEl == null || valEl.isJsonNull()) continue;

                        double value;
                        try {
                            value = valEl.getAsDouble();
                        } catch (Exception ex) {
                            // valore non numerico: skip
                            continue;
                        }

                        ps.setLong(1, ts);
                        ps.setString(2, exchange);
                        ps.setString(3, Symbol);
                        ps.setDouble(4, value);
                        ps.setString(5, "");//Rete
                        ps.setString(6, "");//Address
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            // Query di test: mostra i primi 50 record
            System.out.println("=== Sample from H2 (timestamp | exchange | symbol | prezzo) ===");
            try (Statement st = DatabaseH2.connectionPrezzi.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT timestamp, exchange, symbol, prezzo FROM PrezziNew ORDER BY timestamp LIMIT 50")) {

                while (rs.next()) {
                    long ts = rs.getLong("timestamp");
                    String ex = rs.getString("exchange");
                    String sym = rs.getString("symbol");
                    double v = rs.getDouble("prezzo");
                    System.out.printf("%d | %s | %s = %.6f%n", ts, ex, sym, v);
                }
            }
        //}

        } catch (IOException | InterruptedException e) {
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
            if(CDC_Grafica.InterrompiCiclo)return null;
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
            RT[21] = "Rete di provenienza : "+ network;                  // Rete
            RT[35] = ""; RT[36] = ""; 
            RT[37] = address; 
            RT[38] = "";
            RT[39] = "A"; //Fonte dati A = API Exchange  

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
        }

        return lista;
    }    
    

public static List<String[]> convertBinanceMovimentiFiat(JsonObject JObjetc,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
        //Dentro orders ci sono i depositi e prelievi FIAT
        JsonArray orders= JObjetc.has("orders") ? JObjetc.getAsJsonArray("orders") : new JsonArray();
        //Dentro payments ci sono invece  gli acquisti e vendite crypto tramite fiat che non passano per i trades
        //Ad esempio acquisti con carte
        JsonArray payments= JObjetc.has("payments") ? JObjetc.getAsJsonArray("payments") : new JsonArray();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> ordersOBJ = new ArrayList<>();
        List<JsonObject> paymentsOBJ = new ArrayList<>();
        if (orders==null&&payments==null)return lista;
        
        
        for (JsonElement el : orders) {
            ordersOBJ.add(el.getAsJsonObject());
        }
        
        for (JsonElement el : payments) {
            paymentsOBJ.add(el.getAsJsonObject());
        }
        
        ordersOBJ.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.has("updateTime") ? o1.get("updateTime").getAsString() : o1.get("createTime").getAsString()
            );
            long t2 = Long.parseLong(
                o2.has("updateTime") ? o2.get("updateTime").getAsString() : o2.get("createTime").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        paymentsOBJ.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.has("updateTime") ? o1.get("updateTime").getAsString() : o1.get("createTime").getAsString()
            );
            long t2 = Long.parseLong(
                o2.has("updateTime") ? o2.get("updateTime").getAsString() : o2.get("createTime").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        //Cominciamo dai depositi prelievi FIAT
        int totMov = 1;
        //int i = 1;
        String OldData="0";

        for (JsonElement el : ordersOBJ) {
            if(CDC_Grafica.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("fiatCurrency", "");
            String amount = obj.optString("indicatedAmount", "");
            String amountp = obj.optString("amount", "");
            String feeamount = obj.optString("totalFee", "");
            String metodo = obj.optString("method", "");//Metodo di pagamento
            String direzione = obj.optString("movimento", "");//può essere deposito o prelievo
            String insertTime = obj.optString("updateTime", "createTime");
            String status=obj.optString("status", "");//deve essere Successful perchè sia valido


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

            if (status.trim().equalsIgnoreCase("Successful")) {
                Moneta Mon = new Moneta();
                Mon.Moneta = coin;
                Mon.Tipo = "FIAT";
                Mon.Qta = amount;
                // Calcolo prezzo transazione - qui lo lasciamo vuoto oppure 0
                Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, time, null, true, 2, null);

                String[] RT = new String[Importazioni.ColonneTabella];
                RT[1] = dataa;                                               // Data e ora
                RT[2] = "1 di 2";                                 // Numero movimenti
                RT[3] = Exchange;                                            // Exchange
                RT[4] = "Principale";                                        // Wallet
                RT[7] = "Metodo di pagamento : " + metodo;                     // Causale originale (vuoto)
                if (direzione.equalsIgnoreCase("deposito")) {
                    Mon.Qta = amount;
                    Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, time, null, true, 2, null);
                    RT[0] = dataForId + "_" + Exchange + "_" + totMov + "_1_DF"; // TrasID
                    RT[5] = "DEPOSITO FIAT";
                    RT[6] = "-> " + Mon.Moneta;
                    RT[11] = Mon.Moneta;                                         // Moneta Acq/Ricevuta
                    RT[12] = Mon.Tipo;                                           // Tipo Moneta Acq.
                    RT[13] = Mon.Qta;                                            // Quantità Acq.
                } else //Il fatto che sia prelievo è sottointeso visto che ci sono solo 2 opzioni
                {
                    Mon.Qta = amountp;
                    Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, time, null, true, 2, null);
                    RT[0] = dataForId + "_" + Exchange + "_" + totMov + "_1_PF"; // TrasID
                    RT[5] = "PRELIEVO FIAT";
                    RT[6] = Mon.Moneta + " ->";
                    RT[8] = Mon.Moneta;                                         // Moneta Acq/Ricevuta
                    RT[9] = Mon.Tipo;                                           // Tipo Moneta Acq.
                    RT[10] = ValoreNegativo(Mon.Qta);                                            // Quantità Acq.
                }

                RT[15] = Mon.Prezzo;                                         // Valore in EURO (qui 0)
                RT[22] = "A";                                               // Auto
                RT[29] = insertTime;                                         // Timestamp
                RT[39] = "A"; //Fonte dati A = API Exchange  

                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);

                //Adesso è il turno delle commissioni
                Mon.Qta = feeamount;
                Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, time, null, true, 2, null);
                RT = new String[Importazioni.ColonneTabella];
                RT[1] = dataa;                                               // Data e ora
                RT[2] = "2 di 2";                                   // Numero movimenti
                RT[3] = Exchange;                                            // Exchange
                RT[4] = "Principale";                                        // Wallet
                RT[7] = "";                                                  // Causale originale (vuoto)                
                RT[0] = dataForId + "_" + Exchange + "_" + totMov + "_2_CM"; // TrasID
                RT[5] = "COMMISSIONI";
                RT[6] = Mon.Moneta + " ->";
                RT[8] = Mon.Moneta;                                         // Moneta Acq/Ricevuta
                RT[9] = Mon.Tipo;                                           // Tipo Moneta Acq.
                RT[10] = ValoreNegativo(Mon.Qta);                                            // Quantità Acq.                
                RT[14] = "";                                                 // Valore Mercato originale
                RT[15] = Mon.Prezzo;                                         // Valore in EURO (qui 0)
                RT[22] = "A";                                               // Auto
                RT[29] = insertTime;                                         // Timestamp
                RT[39] = "A"; //Fonte dati A = API Exchange  

                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
        }

        
        
        //Adesso è il turno dei pagamenti
        totMov = 1;
        //int i = 1;
        OldData="0";

        for (JsonElement el : paymentsOBJ) {
            if(CDC_Grafica.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());
            
            boolean inserisciFee=false;
            boolean inserisciArrivoFIAT=false;
            
            Moneta FIAT = new Moneta();
            Moneta CRYPTO = new Moneta();
            Moneta FEE = new Moneta();

            String feeamount = obj.optString("totalFee", "");
            String direzione = obj.optString("movimento", "");//può essere acquisto o vendita
            String souceAmount = obj.optString("sourceAmount", "");
            String metodo = obj.optString("paymentMethod", "");//Metodo di pagamento
            //se il metodo è diverso da Cash Balance devo anche inserire un movimento di DEPOSITO FIAT
            if (!metodo.trim().equalsIgnoreCase("Cash Balance"))inserisciArrivoFIAT=true;
            
            FIAT.Moneta = obj.optString("fiatCurrency", "");
            FIAT.Qta = souceAmount;
            FIAT.Tipo="FIAT";
            CRYPTO.Moneta = obj.optString("cryptoCurrency", "");
            CRYPTO.Qta = obj.optString("obtainAmount", "");
            CRYPTO.Tipo="Crypto";
            
            //Adesso a seconda del movimento calcolo anche la fee
            if (direzione.equalsIgnoreCase("acquisto")){//Acquisto
                //FIAT.Qta=new BigDecimal(FIAT.Qta).subtract(new BigDecimal(feeamount)).toPlainString();
                FEE.Moneta=FIAT.Moneta;
                FEE.Qta=feeamount;
                FEE.Tipo=FIAT.Tipo;
                if (new BigDecimal(FEE.Qta).compareTo(BigDecimal.ZERO)!=0){
                    inserisciFee=true;
                }
                
            }else{//Vendita
                //Non ho i dati nei miei file quindi per le vendite per ora non calcolo le fee ma prendo il prezzo lordo
                inserisciFee=false;
            }

            String insertTime = obj.optString("updateTime", "createTime");
            String status=obj.optString("status", "");//deve essere Completed perchè sia valido


            long time = Long.parseLong(insertTime);
            String data = OperazioniSuDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            int numMovimenti=1;
            int movScambio=1;
            int movCommissione=2;
            if(inserisciArrivoFIAT)
            {
                movScambio++;
                numMovimenti++;
                movCommissione++;
            }
            if(inserisciFee)numMovimenti++;
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);

            if (status.trim().equalsIgnoreCase("Completed")) {
                String[] RT;// = new String[Importazioni.ColonneTabella];
                if (inserisciArrivoFIAT) {
                    //Inserisco l'arrivo nel wallet delle FIAT
                    //String[] RT = new String[Importazioni.ColonneTabella];
                    //FIAT.Qta = feeamount;
                    FIAT.Prezzo = Prezzi.DammiPrezzoTransazione(FIAT, null, time, null, true, 2, null);
                    RT = new String[Importazioni.ColonneTabella];
                    RT[1] = dataa;                                               // Data e ora
                    RT[2] = "1 di "+numMovimenti;                                   // Numero movimenti
                    RT[3] = Exchange;                                            // Exchange
                    RT[4] = "Principale";                                        // Wallet
                    RT[7] = "Metodo di pagamento : "+metodo;                     // Causale originale (vuoto)                
                    RT[0] = dataForId + "_" + Exchange + "_" + totMov + "_1_DF"; // TrasID
                    RT[5] = "DEPOSITO FIAT";
                    RT[6] =  "-> "+FIAT.Moneta;
                    RT[11] = FIAT.Moneta;                                         // Moneta Acq/Ricevuta
                    RT[12] = FIAT.Tipo;                                           // Tipo Moneta Acq.
                    RT[13] = FIAT.Qta;                                            // Quantità Acq.                
                    RT[14] = "";                                                 // Valore Mercato originale
                    RT[15] = FIAT.Prezzo;                                         // Valore in EURO (qui 0)
                    RT[22] = "A";                                               // Auto
                    RT[29] = insertTime;                                         // Timestamp
                    RT[39] = "A"; //Fonte dati A = API Exchange  
                    Importazioni.RiempiVuotiArray(RT);
                    lista.add(RT);

                }


                RT = new String[Importazioni.ColonneTabella];
                RT[1] = dataa;                                                  // Data e ora
                RT[2] = movScambio+" di "+numMovimenti;                         // Numero movimenti
                RT[3] = Exchange;                                               // Exchange
                RT[4] = "Principale";                                           // Wallet
                RT[7] = "Metodo di pagamento : " + metodo;                      // Causale originale (vuoto)
                if (direzione.equalsIgnoreCase("acquisto")){
                    FIAT.Qta=new BigDecimal(FIAT.Qta).subtract(new BigDecimal(feeamount)).toPlainString();
                    RT[0] = dataForId + "_" + Exchange + "_" + totMov + "_"+movScambio+"_AC"; // TrasID
                    RT[5] = "ACQUISTO CRYPTO";
                    RT[6] = FIAT.Moneta + " -> " + CRYPTO.Moneta;
                    RT[8] = FIAT.Moneta;                                        // Moneta Acq/Ricevuta
                    RT[9] = FIAT.Tipo;                                          // Tipo Moneta Acq.
                    RT[10] = ValoreNegativo(FIAT.Qta);                                          // Quantità Acq.
                    RT[11] = CRYPTO.Moneta;                                     // Moneta Acq/Ricevuta
                    RT[12] = CRYPTO.Tipo;                                       // Tipo Moneta Acq.
                    RT[13] = CRYPTO.Qta;                                        // Quantità Acq.
                } else //Il fatto che sia prelievo è sottointeso visto che ci sono solo 2 opzioni
                {
                    RT[0] = dataForId + "_" + Exchange + "_" + totMov + "_"+movScambio+"_VC"; // TrasID
                    RT[5] = "VENDITA CRYPTO";
                    RT[6] = CRYPTO.Moneta + " -> " + FIAT.Moneta;
                    RT[8] = CRYPTO.Moneta;                                      // Moneta Acq/Ricevuta
                    RT[9] = CRYPTO.Tipo;                                        // Tipo Moneta Acq.
                    RT[10] = ValoreNegativo(CRYPTO.Qta);                                        // Quantità Acq.
                    RT[11] = FIAT.Moneta;                                       // Moneta Acq/Ricevuta
                    RT[12] = FIAT.Tipo;                                         // Tipo Moneta Acq.
                    RT[13] = FIAT.Qta;                                          // Quantità Acq.
                }
                FIAT.Prezzo = Prezzi.DammiPrezzoTransazione(FIAT, CRYPTO, time, null, true, 2, null);
                RT[15] = FIAT.Prezzo;                                         // Valore in EURO (qui 0)
                RT[22] = "A";                                               // Auto
                RT[29] = insertTime;                                         // Timestamp
                RT[39] = "A"; //Fonte dati A = API Exchange  

                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);

                //Adesso è il turno delle commissioni
                if (inserisciFee) {
                    FEE.Prezzo = Prezzi.DammiPrezzoTransazione(FEE, null, time, null, true, 2, null);
                    RT = new String[Importazioni.ColonneTabella];
                    RT[1] = dataa;                                               // Data e ora
                    RT[2] = movCommissione+" di "+numMovimenti;                  // Numero movimenti
                    RT[3] = Exchange;                                            // Exchange
                    RT[4] = "Principale";                                        // Wallet
                    RT[7] = "";                                                  // Causale originale (vuoto)                
                    RT[0] = dataForId + "_" + Exchange + "_" + totMov + "_"+movCommissione+"_CM"; // TrasID
                    RT[5] = "COMMISSIONI";
                    RT[6] = FEE.Moneta + " ->";
                    RT[8] = FEE.Moneta;                                         // Moneta Acq/Ricevuta
                    RT[9] = FEE.Tipo;                                           // Tipo Moneta Acq.
                    RT[10] = ValoreNegativo(FEE.Qta);                                            // Quantità Acq.                
                    RT[14] = "";                                                 // Valore Mercato originale
                    RT[15] = FEE.Prezzo;                                         // Valore in EURO (qui 0)
                    RT[22] = "A";                                               // Auto
                    RT[29] = insertTime;                                         // Timestamp
                    RT[39] = "A"; //Fonte dati A = API Exchange  

                    Importazioni.RiempiVuotiArray(RT);
                    lista.add(RT);
                }
            }
        }
        
        
        
        
        return lista;
    }    

    public static String ValoreNegativo(String qta){
        return new BigDecimal(qta).abs().multiply(new BigDecimal(-1)).toPlainString();
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
            if(CDC_Grafica.InterrompiCiclo)return null;
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
            RT[2] = "1 di 2";                                 // Numero movimenti
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
            RT[21] = "Rete di trasferimento : "+ network;                  // Note
            RT[35] = ""; RT[36] = ""; 
            RT[37] = address; 
            RT[38] = ""; 
            RT[39] = "A"; //Fonte dati A = API Exchange  

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
            
            
            //SECONDA PARTE RELATIVA ALLE FEE
            Mon.Qta=new BigDecimal(fee).abs().multiply(new BigDecimal(-1)).toPlainString();
            Mon.Prezzo = Prezzi.DammiPrezzoTransazione(Mon, null, time, null, true, 2, null);
            
            RT = new String[Importazioni.ColonneTabella];
            RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + "2" + "_CM"; // TrasID
            RT[1] = dataa;                                               // Data e ora
            RT[2] = "2 di 2";                                 // Numero movimenti
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
            RT[21] = "Rete di trasferimento : "+ network;                                          // Rete
            RT[35] = ""; RT[36] = ""; 
            RT[37] = address; 
            RT[38] = ""; 
            RT[39] = "A"; //Fonte dati A = API Exchange  

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
            if(CDC_Grafica.InterrompiCiclo)return null;
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
            RT[2] = "1 di 2";                                 // Numero movimenti
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
            RT[38] = ""; 
            RT[39] = "A"; //Fonte dati A = API Exchange  

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
            
            
            //SECONDA PARTE RELATIVA ALLE FEE
            mc.Qta=new BigDecimal(mc.Qta).abs().multiply(new BigDecimal(-1)).toPlainString();
            String PrezzoC = Prezzi.DammiPrezzoTransazione(mc, null, time, null, true, 2, null);
            
            RT = new String[Importazioni.ColonneTabella];
            RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + "2" + "_CM"; // TrasID
            RT[1] = dataa;                                               // Data e ora
            RT[2] = "2 di 2";                                 // Numero movimenti
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
            RT[38] = ""; 
            RT[39] = "A"; //Fonte dati A = API Exchange  

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
        }

        return lista;
    }     
   
      public static List<String[]> convertBinanceConversioniSmall(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
           // System.out.println("Conversioni!!!"+jsonList);
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(o1.get("operateTime").getAsString());
            long t2 = Long.parseLong(o2.get("operateTime").getAsString());
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            if(CDC_Grafica.InterrompiCiclo)return null;
            //System.out.println("Conversioni!!! : "+el);
            JSONObject obj = new JSONObject(el.toString());
            Moneta mu=new Moneta();
            Moneta me=new Moneta();
            
            
          //  String Simboli[] = obj.optString("symbol", "").split("/");

                mu.Moneta=obj.optString("fromAsset", "");
                mu.Qta=obj.optString("amount", "");
                mu.Tipo = (mu.Moneta.equalsIgnoreCase("EUR") || mu.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
                me.Moneta=obj.optString("toAsset", "");
                me.Qta=obj.optString("transferedAmount", "");
                me.Tipo = (me.Moneta.equalsIgnoreCase("EUR") || me.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
 

            mu.Qta=new BigDecimal(mu.Qta).abs().stripTrailingZeros().multiply(new BigDecimal(-1)).toPlainString();
            me.Qta=new BigDecimal(me.Qta).abs().stripTrailingZeros().toPlainString();
            
            Moneta mc=new Moneta();
            mc.Moneta = obj.optString("toAsset", "");
            mc.Qta = new BigDecimal(obj.optString("serviceChargeAmount", "")).stripTrailingZeros().toPlainString();
            mc.Tipo = (mc.Moneta.equalsIgnoreCase("EUR") || mc.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
            
            String Time = obj.optString("operateTime", "");
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
            RT[7] = "asset/dribblet (API)";                              // Causale originale (vuoto)
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
            RT[29] = Time;                                                 // Timestamp
            RT[39] = "A"; //Fonte dati A = API Exchange                      

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
            RT[7] = "asset/dribblet (API)";                                                  // Causale originale (vuoto)
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
            RT[29] = Time;                                         // Timestamp
            RT[39] = "A"; //Fonte dati A = API Exchange  

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
        }

        return lista;
    } 
   
        public static List<String[]> convertBinanceConversioni(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
           // System.out.println("Conversioni!!!"+jsonList);
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(o1.get("createTime").getAsString());
            long t2 = Long.parseLong(o2.get("createTime").getAsString());
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            if(CDC_Grafica.InterrompiCiclo)return null;
            //System.out.println("Conversioni!!! : "+el);
            JSONObject obj = new JSONObject(el.toString());
            Moneta mu=new Moneta();
            Moneta me=new Moneta();
            
            
          //  String Simboli[] = obj.optString("symbol", "").split("/");

                mu.Moneta=obj.optString("fromAsset", "");
                mu.Qta=obj.optString("fromAmount", "");
                mu.Tipo = (mu.Moneta.equalsIgnoreCase("EUR") || mu.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
                me.Moneta=obj.optString("toAsset", "");
                me.Qta=obj.optString("toAmount", "");
                me.Tipo = (me.Moneta.equalsIgnoreCase("EUR") || me.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
 

            mu.Qta=new BigDecimal(mu.Qta).abs().stripTrailingZeros().multiply(new BigDecimal(-1)).toPlainString();
            me.Qta=new BigDecimal(me.Qta).abs().stripTrailingZeros().toPlainString();
                      
            String Time = obj.optString("operateTime", "");
            if (Time.isBlank())Time=obj.optString("createTime", "");
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
            RT[7] = "convert/tradeFlow (API)";                              // Causale originale (vuoto)
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
            RT[29] = Time;                                                 // Timestamp
            RT[39] = "A"; //Fonte dati A = API Exchange                      

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            lista.add(RT);
            
            
        }

        return lista;
    }    
    
   public static List<String[]> convertBinanceEarn(JsonArray jsonList,String Exchange) {
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
            if(CDC_Grafica.InterrompiCiclo)return null;
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
            RT[7] = tipo;                                                  // Causale originale (vuoto)
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
            RT[38] = ""; 
            RT[39] = "A"; //Fonte dati A = API Exchange  

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            //inserisco il movimento solo se il quantitativo è diverso da zero
            if (!Mon.Qta.equals("0")) lista.add(RT);
        }

        return lista;
    }    
   
   
      public static List<String[]> convertBinanceRewards(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.get("divTime").getAsString()
            );
            long t2 = Long.parseLong(
                o2.get("divTime").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            if(CDC_Grafica.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("asset", "");
            String amount = obj.optString("amount", "");
            String tipo = obj.optString("enInfo", "");
            String insertTime = obj.optString("divTime", "");
            
            //Queste tipologie non le voglio conteggiare perchè già conteggiate in altro ciclo
            if (!tipo.equalsIgnoreCase("Flexible")&&!tipo.equalsIgnoreCase("Locked")){
//&&!tipo.equalsIgnoreCase("BNB Vault")
            //System.out.println("Inserito " +amount+" "+coin+" - tipologia:"+tipo);

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
            RT[7] = tipo;                                                  // Causale originale (vuoto)
            if (tipoMoneta.equals("FIAT"))
            {
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_DF"; // TrasID
                RT[5]="DEPOSITO FIAT";
            }else 
                {
                RT[0] = dataForId + "_"+Exchange+"_" + totMov + "_" + i + "_RW"; // TrasID
                RT[5]="EARN";
                if (tipo.toLowerCase().contains("staking"))RT[5]="STAKING";
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
            RT[38] = ""; 
            RT[39] = "A"; //Fonte dati A = API Exchange  

            Importazioni.RiempiVuotiArray(RT);
            //System.out.println(RT[0]);
            if (!Mon.Qta.equals("0")) lista.add(RT);
        }
}
        return lista;
    }    
   
   
   
   

    public static void main(String[] args) {

    }
}

