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
import java.awt.Component;
import java.awt.Cursor;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.json.JSONObject;

public class CcxtInterop {
    
    public static final String NODE_VERSION = "v24.7.0";
    public static final Path NODE_DIR = Paths.get(VarStatiche.getWorkingDirectory()+"tools", "node").toAbsolutePath().normalize();;
    

    
    public static void ensureNodeInstalled() throws IOException {
        

        
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
           // System.out.println("✅ Node.js già presente");
            return;
        }
        System.out.println("Node.js non presente.");
        System.out.println("⬇️ Scarico Node.js standalone...");
        Path downloadPath = Paths.get(VarStatiche.getWorkingDirectory()+"tools", filename);
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
       // System.out.println("nodePath="+nodePath);

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
        //System.out.println("CCXT già installato in: " + ccxtDir);
        return;
    }    
    
    Path npmPath = getNpmPath();
    System.out.println("npmPath="+npmPath);
    

    System.out.println("Installo ccxt...");
    ProcessBuilder builder = new ProcessBuilder(npmPath.toString(), "install", "ccxt");
    System.out.println(VarStatiche.getWorkingDirectory() + "tools/node");
    builder.directory(NODE_DIR.toFile());
    //builder.directory(nodeModulesDir.toFile());
    //builder.directory(new File(Statiche.getWorkingDirectory() + "tools/node"));  // directory di lavoro
    System.out.println("Comando: " + String.join(" ", builder.command()));
    System.out.println("Working directory: " + builder.directory().getAbsolutePath());
    System.out.println("Attendere, la prima installazione potrebbe durare diversi minuti");
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
    System.out.println(VarStatiche.getWorkingDirectory() + "tools/node");
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
    
    
    public static Path getNpmPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return NODE_DIR.resolve("node-" + NODE_VERSION + "-win-x64").resolve("npm.cmd");
        } else {
            return NODE_DIR.resolve("node-" + NODE_VERSION + "-" + (os.contains("mac") ? "darwin-x64" : "linux-x64"))
                           .resolve("bin").resolve("npm");
        }
    }

    
public static Path getNodeExePath() {
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
            Logger.getLogger(Principale.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Principale.class.getName()).log(Level.SEVERE, null, ex);
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
            if(!Principale.InterrompiCiclo)
                Importazioni.ScriviListaSuMappaCrypto(lista,true);
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
            for (String[] movimento : Principale.MappaCryptoWallet.values()) {
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
            //String estrazioni[] = new String[]{"Binance_StakingSOL"};
            int chiamate=estrazioni.length+1;
            int j=0;
            for (String script : estrazioni) {
                //Interrompo la funzione se ho premuto interrompi o se ho degli errori bloccanti sulla funzione
                if (Principale.InterrompiCiclo||progress.ErroriNodeJS())
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
            if (Principale.InterrompiCiclo||progress.ErroriNodeJS())
                {
                    JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                    return;
                }

            //4 - IMPORTO TUTTO NEL DATABASE
            //Recuperati tutti i movimenti posso procedere all'aggiunta al database vera e propria
            //Se non è andato tutto a buon fine non porto a termine l'importazione
            //Importazioni.inserisciListaMovimentisuMappaCryptoWallet(lista);
            int risultato[]=Importazioni.ScriviListaSuMappaCrypto(lista,true);
            if (risultato[0]!=0) 
            {
                Principale.TabellaCryptodaAggiornare=true;
                JOptionPane.showConfirmDialog(null, 
                    "Impot terminato, sono stati inseriti "+risultato[0]+" nuovi movimenti.",
                    "Messaggio",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
            }
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
        Path scriptPath = Paths.get(VarStatiche.getPathRisorse()
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
                    if(Principale.InterrompiCiclo)
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

public static void recuperPrezzi_OLD(String Symbol,long timestamp) throws SQLException {

        //long timestampAttuale = System.currentTimeMillis();
        //Voglio reperire sempre almeno 1h di dati per cui prendo la mezz'ora prima e la mezz'ora dopo il timestamp indicato
        long Since=timestamp-1800000;
        long Until=timestamp+1800000;
        //Lista degli exchange a cui richiedere il prezzo della cripto
        String exchanges="binance,cryptocom,bybit,okx,coinbase,bitstamp,kucoin";
        
        Path nodePath = getNodeExePath();
        Path scriptPath = Paths.get(VarStatiche.getPathRisorse()
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

        System.out.println("Scarico Prezzi di "+Symbol+" in data "+FunzioniDate.ConvertiDatadaLongAlSecondo(timestamp));
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

    /*        // Query di test: mostra i primi 50 record
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
            }*/
        //}

        } catch (IOException | InterruptedException e) {
            LoggerGC.ScriviErrore(e);
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
            if(Principale.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("coin", "");
            String amount = obj.optString("amount", "");
            String network = obj.optString("network", "");
            String txId = obj.optString("txId", "");
            String address = obj.optString("address", "");
            String insertTime = obj.optString("insertTime", "completeTime");
            //String completeTime = obj.optString("completeTime", insertTime);

            long time = Long.parseLong(insertTime);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
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

            String[] RT = MovimentiCrypto.creaMovimento(null, Mon, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    "Rete di provenienza : "+ network, "A", txId, null, null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[37] = address;
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
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
            if(Principale.InterrompiCiclo)return null;
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
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
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
                String[] RT;
                if (direzione.equalsIgnoreCase("deposito")) {
                    Mon.Qta = amount;
                    RT = MovimentiCrypto.creaMovimento(null, Mon, Exchange, "Principale",
                            time, null, null, totMov, 1, null,
                            null, "A", null, null, null);
                } else //Il fatto che sia prelievo è sottointeso visto che ci sono solo 2 opzioni
                {
                    Mon.Qta = ValoreNegativo(amountp);
                    RT = MovimentiCrypto.creaMovimento(Mon, null, Exchange, "Principale",
                            time, null, null, totMov, 1, null,
                            null, "A", null, null, null);
                }
                if (RT != null) {
                    RT[2] = "1 di 2";                                        // Numero movimenti
                    RT[7] = "Metodo di pagamento : " + metodo;                // Causale originale
                    RT[39] = "A"; //Fonte dati A = API Exchange
                    Importazioni.RiempiVuotiArray(RT);
                    lista.add(RT);
                }

                //Adesso è il turno delle commissioni
                Moneta Fee = new Moneta();
                Fee.Moneta = coin;
                Fee.Tipo = "FIAT";
                Fee.Qta = ValoreNegativo(feeamount);
                RT = MovimentiCrypto.creaMovimento(Fee, null, Exchange, "Principale",
                        time, null, null, totMov, 2, null,
                        null, "A", null, "COMMISSIONE", null);
                if (RT != null) {
                    RT[2] = "2 di 2";                                        // Numero movimenti
                    RT[39] = "A"; //Fonte dati A = API Exchange
                    Importazioni.RiempiVuotiArray(RT);
                    lista.add(RT);
                }
            }
        }

        
        
        //Adesso è il turno dei pagamenti
        totMov = 1;
        //int i = 1;
        OldData="0";

        for (JsonElement el : paymentsOBJ) {
            if(Principale.InterrompiCiclo)return null;
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
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
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
                String[] RT;
                if (inserisciArrivoFIAT) {
                    //Inserisco l'arrivo nel wallet delle FIAT
                    RT = MovimentiCrypto.creaMovimento(null, FIAT, Exchange, "Principale",
                            time, null, null, totMov, 1, null,
                            null, "A", null, null, null);
                    if (RT != null) {
                        RT[2] = "1 di "+numMovimenti;                          // Numero movimenti
                        RT[7] = "Metodo di pagamento : "+metodo;               // Causale originale
                        RT[39] = "A"; //Fonte dati A = API Exchange
                        Importazioni.RiempiVuotiArray(RT);
                        lista.add(RT);
                    }
                }

                if (direzione.equalsIgnoreCase("acquisto")){
                    FIAT.Qta=ValoreNegativo(new BigDecimal(FIAT.Qta).subtract(new BigDecimal(feeamount)).toPlainString());
                    RT = MovimentiCrypto.creaMovimento(FIAT, CRYPTO, Exchange, "Principale",
                            time, null, null, totMov, movScambio, null,
                            null, "A", null, null, null);
                } else //Il fatto che sia prelievo è sottointeso visto che ci sono solo 2 opzioni
                {
                    CRYPTO.Qta=ValoreNegativo(CRYPTO.Qta);
                    RT = MovimentiCrypto.creaMovimento(CRYPTO, FIAT, Exchange, "Principale",
                            time, null, null, totMov, movScambio, null,
                            null, "A", null, null, null);
                }
                if (RT != null) {
                    RT[2] = movScambio+" di "+numMovimenti;                    // Numero movimenti
                    RT[7] = "Metodo di pagamento : " + metodo;                 // Causale originale
                    RT[39] = "A"; //Fonte dati A = API Exchange
                    Importazioni.RiempiVuotiArray(RT);
                    lista.add(RT);
                }

                //Adesso è il turno delle commissioni
                if (inserisciFee) {
                    FEE.Qta=ValoreNegativo(FEE.Qta);
                    RT = MovimentiCrypto.creaMovimento(FEE, null, Exchange, "Principale",
                            time, null, null, totMov, movCommissione, null,
                            null, "A", null, "COMMISSIONE", null);
                    if (RT != null) {
                        RT[2] = movCommissione+" di "+numMovimenti;            // Numero movimenti
                        RT[39] = "A"; //Fonte dati A = API Exchange
                        Importazioni.RiempiVuotiArray(RT);
                        lista.add(RT);
                    }
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
            if(Principale.InterrompiCiclo)return null;
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
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
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
            Mon.Qta=ValoreNegativo(amount);

            String[] RT = MovimentiCrypto.creaMovimento(Mon, null, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    "Rete di trasferimento : "+ network, "A", txId, null, null);
            if (RT != null) {
                RT[2] = "1 di 2";                                        // Numero movimenti
                RT[37] = address;
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }

            //SECONDA PARTE RELATIVA ALLE FEE
            Moneta Fee=new Moneta();
            Fee.Moneta=coin;
            Fee.Tipo=tipoMoneta;
            Fee.Qta=ValoreNegativo(fee);
            RT = MovimentiCrypto.creaMovimento(Fee, null, Exchange, "Principale",
                    time, null, null, totMov, 2, null,
                    "Rete di trasferimento : "+ network, "A", txId, "COMMISSIONE", null);
            if (RT != null) {
                RT[2] = "2 di 2";                                        // Numero movimenti
                RT[37] = address;
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
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
            if(Principale.InterrompiCiclo)return null;
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
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            String[] RT = MovimentiCrypto.creaMovimento(mu, me, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    null, "A", null, null, null);
            if (RT != null) {
                RT[2] = "1 di 2";                                        // Numero movimenti
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }

            //SECONDA PARTE RELATIVA ALLE FEE
            mc.Qta=new BigDecimal(mc.Qta).abs().multiply(new BigDecimal(-1)).toPlainString();
            RT = MovimentiCrypto.creaMovimento(mc, null, Exchange, "Principale",
                    time, null, null, totMov, 2, null,
                    null, "A", null, "COMMISSIONE", null);
            if (RT != null) {
                RT[2] = "2 di 2";                                        // Numero movimenti
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
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
            if(Principale.InterrompiCiclo)return null;
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
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            String[] RT = MovimentiCrypto.creaMovimento(mu, me, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    null, "A", null, null, null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[7] = "asset/dribblet (API)";                          // Causale originale
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }

            //SECONDA PARTE RELATIVA ALLE FEE
            mc.Qta=new BigDecimal(mc.Qta).abs().multiply(new BigDecimal(-1)).toPlainString();
            RT = MovimentiCrypto.creaMovimento(mc, null, Exchange, "Principale",
                    time, null, null, totMov, 2, null,
                    null, "A", null, "COMMISSIONE", null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[7] = "asset/dribblet (API)";                          // Causale originale
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
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
            if(Principale.InterrompiCiclo)return null;
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
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            String[] RT = MovimentiCrypto.creaMovimento(mu, me, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    null, "A", null, null, null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[7] = "convert/tradeFlow (API)";                       // Causale originale
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
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
            if(Principale.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("asset", "");
            String amount = obj.optString("amount", "");
            amount = obj.optString("rewards", amount);
            String tipo = obj.optString("type", "");
            String insertTime = obj.optString("time", "");


            long time = Long.parseLong(insertTime);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
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

            String[] RT = MovimentiCrypto.creaMovimento(null, Mon, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    null, "A", null, tipoMoneta.equals("FIAT") ? null : "EARN", null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[7] = tipo;                                            // Causale originale
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
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
            if(Principale.InterrompiCiclo)return null;
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
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
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

            String TipoTr = null;
            if (!tipoMoneta.equals("FIAT")) {
                TipoTr = tipo.toLowerCase().contains("staking") ? "STAKING REWARD" : "EARN";
            }

            String[] RT = MovimentiCrypto.creaMovimento(null, Mon, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    null, "A", null, TipoTr, null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[7] = tipo;                                            // Causale originale
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
        }
}
        return lista;
    }
   
   
   
   

    public static void main(String[] args) {

    }
}

